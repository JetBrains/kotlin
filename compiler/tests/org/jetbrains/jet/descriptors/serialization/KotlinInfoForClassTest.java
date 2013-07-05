/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.descriptors.serialization;

import jet.KotlinInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.codegen.CodegenTestCase;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;

import java.io.IOException;

import static com.google.protobuf.ExtensionRegistryLite.getEmptyRegistry;

public class KotlinInfoForClassTest extends CodegenTestCase {
    public static final FqName NAMESPACE_NAME = new FqName("test");
    public static final FqNameUnsafe CLASS_NAME = new FqNameUnsafe("A");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
    }

    public void testClassKotlinInfo() throws Exception {
        loadText("package " + NAMESPACE_NAME + "\n" +
                 "\n" +
                 "class " + CLASS_NAME + " {\n" +
                 "    fun foo() {}\n" +
                 "    fun bar() = 42\n" +
                 "}\n");
        Class aClass = generateClass(NAMESPACE_NAME + "." + CLASS_NAME);

        assertTrue(aClass.isAnnotationPresent(KotlinInfo.class));
        KotlinInfo kotlinInfo = (KotlinInfo) aClass.getAnnotation(KotlinInfo.class);

        AbstractDescriptorFinder descriptorFinder = new KotlinInfoBasedDescriptorFinder(kotlinInfo);

        ClassDescriptor descriptor = descriptorFinder.findClass(new ClassId(NAMESPACE_NAME, CLASS_NAME));
        assertNotNull(descriptor);
        assertEquals(CLASS_NAME.asString(), descriptor.getName().asString());
    }

    private static class KotlinInfoBasedDescriptorFinder extends AbstractDescriptorFinder {
        private final ClassData classData;
        private final NamespaceDescriptorImpl namespace;

        public KotlinInfoBasedDescriptorFinder(@NotNull KotlinInfo kotlinInfo) throws IOException {
            super(new LockBasedStorageManager(), AnnotationDeserializer.UNSUPPORTED);

            this.classData = ClassData.read(kotlinInfo.data(), getEmptyRegistry());
            this.namespace = JetTestUtils.createTestNamespace(NAMESPACE_NAME.shortName());
        }

        @Nullable
        @Override
        protected ClassData getClassData(@NotNull ClassId classId) {
            assert classId.getPackageFqName().equals(NAMESPACE_NAME) &&
                   classId.getRelativeClassName().equals(CLASS_NAME) : "Unsupported classId: " + classId;
            return classData;
        }

        @NotNull
        @Override
        protected NamespaceDescriptor getPackage(@NotNull FqName fqName) {
            assert fqName.equals(NAMESPACE_NAME) : "Unsupported namespace: " + fqName;
            return namespace;
        }

        @Override
        protected void classDescriptorCreated(@NotNull ClassDescriptor classDescriptor) {
        }
    }
}
