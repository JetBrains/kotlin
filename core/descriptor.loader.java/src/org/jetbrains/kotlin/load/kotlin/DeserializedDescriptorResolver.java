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

package org.jetbrains.kotlin.load.kotlin;

import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.ChainedScope;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.serialization.ClassData;
import org.jetbrains.kotlin.serialization.PackageData;
import org.jetbrains.kotlin.serialization.deserialization.ClassDataProvider;
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents;
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil;

import javax.inject.Inject;
import java.util.*;

import static kotlin.KotlinPackage.setOf;
import static org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.*;

public final class DeserializedDescriptorResolver {
    private final ErrorReporter errorReporter;
    private DeserializationComponents components;

    public static final Set<KotlinClassHeader.Kind> KOTLIN_CLASS = setOf(CLASS);
    public static final Set<KotlinClassHeader.Kind> KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART = setOf(FILE_FACADE, MULTIFILE_CLASS_PART);
    public static final Set<KotlinClassHeader.Kind> KOTLIN_PACKAGE_FACADE = setOf(PACKAGE_FACADE);

    public DeserializedDescriptorResolver(@NotNull ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    // component dependency cycle
    @Inject
    public void setComponents(@NotNull DeserializationComponentsForJava context) {
        this.components = context.getComponents();
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull KotlinJvmBinaryClass kotlinClass) {
        String[] data = readData(kotlinClass, KOTLIN_CLASS);
        if (data != null) {
            ClassData classData = JvmProtoBufUtil.readClassDataFrom(data);
            KotlinJvmBinarySourceElement sourceElement = new KotlinJvmBinarySourceElement(kotlinClass);
            ClassDataProvider classDataProvider = new ClassDataProvider(classData, sourceElement);
            return components.getClassDeserializer().deserializeClass(kotlinClass.getClassId(), classDataProvider);
        }
        return null;
    }

    @Nullable
    public JetScope createKotlinPackagePartScope(@NotNull PackageFragmentDescriptor descriptor, @NotNull KotlinJvmBinaryClass kotlinClass) {
        String[] data = readData(kotlinClass, KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART);
        if (data != null) {
            //all classes are included in java scope
            PackageData packageData = JvmProtoBufUtil.readPackageDataFrom(data);
            return new DeserializedPackageMemberScope(
                    descriptor, packageData.getPackageProto(), packageData.getNameResolver(), components,
                    new Function0<Collection<Name>>() {
                        @Override
                        public Collection<Name> invoke() {
                            return Collections.emptyList();
                        }
                    }
            );
        }
        return null;
    }

    @NotNull
    public JetScope createKotlinPackageScope(@NotNull PackageFragmentDescriptor descriptor, @NotNull List<KotlinJvmBinaryClass> packageParts) {
        List<JetScope> list = new ArrayList<JetScope>();
        for (KotlinJvmBinaryClass callable : packageParts) {
            JetScope scope = createKotlinPackagePartScope(descriptor, callable);
            if (scope != null) {
                list.add(scope);
            }
        }
        if (list.isEmpty()) {
            return JetScope.Empty.INSTANCE$;
        }
        return new ChainedScope(descriptor, "Member scope for union of package parts data", list.toArray(new JetScope[list.size()]));
    }

    @Nullable
    public String[] readData(@NotNull KotlinJvmBinaryClass kotlinClass, @NotNull Set<KotlinClassHeader.Kind> expectedKinds) {
        KotlinClassHeader header = kotlinClass.getClassHeader();
        if (!header.getIsCompatibleAbiVersion()) {
            errorReporter.reportIncompatibleAbiVersion(kotlinClass.getClassId(), kotlinClass.getLocation(), header.getVersion());
        }
        else if (expectedKinds.contains(header.getKind())) {
            return header.getAnnotationData();
        }

        return null;
    }
}
