/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.psi.JetProperty;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.serialization.*;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.serialization.jvm.BitEncoding;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.ABI_VERSION_FIELD_NAME;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class PackagePartCodegen extends MemberCodegen<JetFile> {
    private final Type packagePartType;

    public PackagePartCodegen(
            @NotNull ClassBuilder v,
            @NotNull JetFile file,
            @NotNull Type packagePartType,
            @NotNull FieldOwnerContext context,
            @NotNull GenerationState state
    ) {
        super(state, null, context, file, v);
        this.packagePartType = packagePartType;
    }

    @Override
    protected void generateDeclaration() {
        v.defineClass(element, V1_6,
                      ACC_PUBLIC | ACC_FINAL,
                      packagePartType.getInternalName(),
                      null,
                      "java/lang/Object",
                      ArrayUtil.EMPTY_STRING_ARRAY
        );
        v.visitSource(element.getName(), null);

        generatePropertyMetadataArrayFieldIfNeeded(packagePartType);
    }

    @Override
    protected void generateBody() {
        for (JetDeclaration declaration : element.getDeclarations()) {
            if (declaration instanceof JetNamedFunction || declaration instanceof JetProperty) {
                genFunctionOrProperty(declaration);
            }
        }

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            generateInitializers(new Function0<ExpressionCodegen>() {
                @Override
                public ExpressionCodegen invoke() {
                    return createOrGetClInitCodegen();
                }
            });
        }
    }

    @Override
    protected void generateKotlinAnnotation() {
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) {
            return;
        }

        List<DeclarationDescriptor> members = new ArrayList<DeclarationDescriptor>();
        for (JetDeclaration declaration : element.getDeclarations()) {
            if (declaration instanceof JetNamedFunction) {
                SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, declaration);
                members.add(functionDescriptor);
            } else if (declaration instanceof JetProperty) {
                VariableDescriptor property = bindingContext.get(BindingContext.VARIABLE, declaration);
                members.add(property);
            }
        }

        JvmSerializationBindings bindings = v.getSerializationBindings();

        DescriptorSerializer serializer = DescriptorSerializer.createTopLevel(new JvmSerializerExtension(bindings, state.getTypeMapper()));
        Collection<PackageFragmentDescriptor> packageFragments = Lists.newArrayList();
        //ContainerUtil.addIfNotNull(packageFragments, packageFragment);
        //ContainerUtil.addIfNotNull(packageFragments, compiledPackageFragment);

        ProtoBuf.Package packageProto = serializer.packagePartProto(members, new Function1<DeclarationDescriptor, Boolean>() {
            @Override
            public Boolean invoke(DeclarationDescriptor descriptor) {
                //return !(descriptor instanceof CallableMemberDescriptor && relevantCallables.contains(descriptor));
                return false;
            }
        }).build();

        if (packageProto.getMemberCount() == 0) return;

        StringTable strings = serializer.getStringTable();
        NameResolver nameResolver = new NameResolver(strings.serializeSimpleNames(), strings.serializeQualifiedNames());
        PackageData data = new PackageData(nameResolver, packageProto);

        AnnotationVisitor av = v.newAnnotation(asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_FILE_FACADE), true);
        av.visit(ABI_VERSION_FIELD_NAME, JvmAbi.VERSION);
        AnnotationVisitor array = av.visitArray(JvmAnnotationNames.DATA_FIELD_NAME);
        for (String string : BitEncoding.encodeBytes(SerializationUtil.serializePackageData(data))) {
            array.visit(null, string);
        }
        array.visitEnd();
        av.visitEnd();
    }
}
