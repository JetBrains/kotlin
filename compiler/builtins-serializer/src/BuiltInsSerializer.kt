/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.utils.builtinsSerializer

import java.io.File
import java.io.PrintStream
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.descriptors.serialization.DescriptorSerializer
import org.jetbrains.jet.descriptors.serialization.SerializerExtension
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import org.jetbrains.jet.lang.types.lang.BuiltInsSerializationUtil
import org.jetbrains.jet.descriptors.serialization.NameSerializationUtil
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import com.intellij.openapi.Disposable
import org.jetbrains.jet.cli.common.CLIConfigurationKeys
import org.jetbrains.jet.config.CommonConfigurationKeys
import org.jetbrains.jet.cli.common.messages.MessageCollector
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.resolve.name.FqName
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.lang.BuiltInsPackageMigration

public class BuiltInsSerializer(val out: PrintStream?) {
    private var totalSize = 0
    private var totalFiles = 0

    public fun serialize(destDir: File, srcDirs: Collection<File>) {
        val rootDisposable = Disposer.newDisposable()
        try {
            serialize(rootDisposable, destDir, srcDirs)
        }
        finally {
            Disposer.dispose(rootDisposable)
        }
    }

    fun serialize(disposable: Disposable, destDir: File, srcDirs: Collection<File>) {
        val configuration = CompilerConfiguration()
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        val sourceRoots = srcDirs map { it.path }
        configuration.put(CommonConfigurationKeys.SOURCE_ROOTS_KEY, sourceRoots)

        val environment = JetCoreEnvironment.createForTests(disposable, configuration)

        val files = environment.getSourceFiles() ?: error("No source files in $sourceRoots")

        val project = environment.getProject()

        val session = AnalyzerFacadeForJVM.createLazyResolveSession(project, files, BindingTraceContext(), false)
        val module = session.getModuleDescriptor()

        if (!FileUtil.delete(destDir)) {
            System.err.println("Could not delete: " + destDir)
        }
        if (!destDir.mkdirs()) {
            System.err.println("Could not make directories: " + destDir)
        }

        for (fqName in ContainerUtil.mapNotNull(files) { it?.getPackageName() }.toSet()) {
            if (fqName == "kotlin") {
                BuiltInsPackageMigration.isSerializingBuiltInsInKotlinPackage = true
                val provider = session.getDeclarationProviderFactory().getPackageMemberDeclarationProvider(FqName(fqName))!!

                fun resolveClass(name: String): ClassDescriptor {
                    val classes = provider.getClassOrObjectDeclarations(Name.identifier(name))
                    assert(classes.size() == 1, "Class $name not found: " + classes)
                    return session.getClassDescriptor(classes.iterator().next())
                }
                BuiltInsPackageMigration.anyType = resolveClass("Any").getDefaultType()
                BuiltInsPackageMigration.stringType = resolveClass("String").getDefaultType()
                BuiltInsPackageMigration.annotationType = resolveClass("Annotation").getDefaultType()
                BuiltInsPackageMigration.arrayClass = resolveClass("Array")
                BuiltInsPackageMigration.enumClass = resolveClass("Enum")
            }
            else {
                BuiltInsPackageMigration.isSerializingBuiltInsInKotlinPackage = false
            }
            serializePackage(module, FqName(fqName), destDir)
        }

        out?.println("Total bytes written: $totalSize to $totalFiles files")
    }

    fun serializePackage(module: ModuleDescriptor, fqName: FqName, destDir: File) {
        val packageView = module.getPackage(fqName) ?: error("No package resolved in $module")

        // TODO: perform some kind of validation? At the moment not possible because DescriptorValidator is in compiler-tests
        // DescriptorValidator.validate(packageView)

        val serializer = DescriptorSerializer(object : SerializerExtension() {
            override fun hasSupertypes(descriptor: ClassDescriptor): Boolean =
                    !KotlinBuiltIns.isSpecialClassWithNoSupertypes(descriptor)
        })

        val classNames = ArrayList<Name>()
        val allDescriptors = DescriptorSerializer.sort(packageView.getMemberScope().getAllDescriptors())

        ClassSerializationUtil.serializeClasses(allDescriptors, serializer, object : ClassSerializationUtil.Sink {
            override fun writeClass(classDescriptor: ClassDescriptor, classProto: ProtoBuf.Class) {
                val stream = ByteArrayOutputStream()
                classProto.writeTo(stream)
                write(destDir, getFileName(classDescriptor), stream)

                if (DescriptorUtils.isTopLevelDeclaration(classDescriptor)) {
                    classNames.add(classDescriptor.getName())
                }
            }
        })

        val classNamesStream = ByteArrayOutputStream()
        writeClassNames(serializer, classNames, classNamesStream)
        write(destDir, BuiltInsSerializationUtil.getClassNamesFilePath(fqName), classNamesStream)

        val packageStream = ByteArrayOutputStream()
        val fragments = module.getPackageFragmentProvider().getPackageFragments(fqName)
        val packageProto = serializer.packageProto(fragments).build() ?: error("Package fragments not serialized: $fragments")
        packageProto.writeTo(packageStream)
        write(destDir, BuiltInsSerializationUtil.getPackageFilePath(fqName), packageStream)

        val nameStream = ByteArrayOutputStream()
        NameSerializationUtil.serializeNameTable(nameStream, serializer.getNameTable())
        write(destDir, BuiltInsSerializationUtil.getNameTableFilePath(fqName), nameStream)
    }

    fun writeClassNames(serializer: DescriptorSerializer, classNames: List<Name>, stream: ByteArrayOutputStream) {
        val nameTable = serializer.getNameTable()
        DataOutputStream(stream) use { output ->
            output.writeInt(classNames.size())
            for (className in classNames) {
                output.writeInt(nameTable.getSimpleNameIndex(className))
            }
        }
    }

    fun write(destDir: File, fileName: String, stream: ByteArrayOutputStream) {
        totalSize += stream.size()
        totalFiles++
        FileUtil.writeToFile(File(destDir, fileName), stream.toByteArray())
    }

    fun getFileName(classDescriptor: ClassDescriptor): String {
        return BuiltInsSerializationUtil.getClassMetadataPath(ClassSerializationUtil.getClassId(classDescriptor))
    }
}
