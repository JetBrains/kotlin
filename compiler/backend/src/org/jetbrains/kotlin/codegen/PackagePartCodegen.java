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

import com.intellij.util.ArrayUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.annotation.AnnotatedSimple;
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext;
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.writeAnnotationData;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class PackagePartCodegen extends MemberCodegen<KtFile> {
    private final Type packagePartType;

    public PackagePartCodegen(
            @NotNull ClassBuilder v,
            @NotNull KtFile file,
            @NotNull Type packagePartType,
            @NotNull FieldOwnerContext context,
            @NotNull GenerationState state
    ) {
        super(state, null, context, file, v);
        this.packagePartType = packagePartType;
    }

    @Override
    protected void generateDeclaration() {
        v.defineClass(element, state.getClassFileVersion(),
                      ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
                      packagePartType.getInternalName(),
                      null,
                      "java/lang/Object",
                      ArrayUtil.EMPTY_STRING_ARRAY
        );
        v.visitSource(element.getName(), null);

        generatePropertyMetadataArrayFieldIfNeeded(packagePartType);

        generateAnnotationsForPartClass();
    }

    private void generateAnnotationsForPartClass() {
        List<AnnotationDescriptor> fileAnnotationDescriptors = new ArrayList<>();
        for (KtAnnotationEntry annotationEntry : element.getAnnotationEntries()) {
            AnnotationDescriptor annotationDescriptor = state.getBindingContext().get(BindingContext.ANNOTATION, annotationEntry);
            if (annotationDescriptor != null) {
                fileAnnotationDescriptors.add(annotationDescriptor);
            }
        }
        Annotated annotatedFile = new AnnotatedSimple(new AnnotationsImpl(fileAnnotationDescriptors));
        AnnotationCodegen.forClass(v.getVisitor(), this,  state.getTypeMapper()).genAnnotations(annotatedFile, null);
    }

    @Override
    protected void generateBody() {
        for (KtDeclaration declaration : element.getDeclarations()) {
            if (declaration.hasModifier(KtTokens.HEADER_KEYWORD)) continue;

            if (declaration instanceof KtNamedFunction || declaration instanceof KtProperty || declaration instanceof KtTypeAlias) {
                genSimpleMember(declaration);
            }
        }

        if (state.getClassBuilderMode().generateBodies) {
            generateInitializers(this::createOrGetClInitCodegen);
        }
    }

    @Override
    protected void generateKotlinMetadataAnnotation() {
        List<DeclarationDescriptor> members = new ArrayList<>();
        for (KtDeclaration declaration : element.getDeclarations()) {
            if (declaration instanceof KtNamedFunction) {
                SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, declaration);
                members.add(functionDescriptor);
            }
            else if (declaration instanceof KtProperty) {
                VariableDescriptor property = bindingContext.get(BindingContext.VARIABLE, declaration);
                members.add(property);
            }
            else if (declaration instanceof KtTypeAlias) {
                TypeAliasDescriptor typeAlias = bindingContext.get(BindingContext.TYPE_ALIAS, declaration);
                members.add(typeAlias);
            }
        }

        DescriptorSerializer serializer =
                DescriptorSerializer.createTopLevel(new JvmSerializerExtension(v.getSerializationBindings(), state));
        ProtoBuf.Package packageProto = serializer.packagePartProto(element.getPackageFqName(), members).build();

        WriteAnnotationUtilKt.writeKotlinMetadata(v, state, KotlinClassHeader.Kind.FILE_FACADE, 0, av -> {
            writeAnnotationData(av, serializer, packageProto);
            return Unit.INSTANCE;
        });
    }

    @Override
    protected void generateSyntheticPartsAfterBody() {
        generateSyntheticAccessors();
    }
}
