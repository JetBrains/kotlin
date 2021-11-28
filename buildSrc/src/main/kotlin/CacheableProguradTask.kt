/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import groovy.lang.Closure
import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.kotlin.dsl.property
import proguard.ClassSpecification
import java.io.File

@CacheableTask
open class CacheableProguardTask : proguard.gradle.ProGuardTask() {

    @get:Internal
    val javaLauncher: Property<JavaLauncher> = project.objects.property()

    @get:Internal
    val jdkHomePath: Provider<File> = javaLauncher.map { it.metadata.installationPath.asFile }

    @get:Optional
    @get:Input
    internal val jdkMajorVersion: Provider<JavaVersion> = javaLauncher.map {
        JavaVersion.toVersion(it.metadata.languageVersion.toString())
    }

    @CompileClasspath
    override fun getLibraryJarFileCollection(): FileCollection = super.getLibraryJarFileCollection()
        .filter { libraryFile ->
            jdkHomePath.orNull?.let { !libraryFile.absoluteFile.startsWith(it.absoluteFile) } ?: true
        }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getConfigurationFileCollection(): FileCollection = super.getConfigurationFileCollection()

    @InputFiles
    @Classpath
    override fun getInJarFileCollection(): FileCollection = super.getInJarFileCollection()

    @Optional
    @OutputFiles
    override fun getOutJarFileCollection(): FileCollection = super.getOutJarFileCollection()

    @get:Optional
    @get:OutputFile
    internal val printConfigurationFile: File?
        get() = configuration.printConfiguration?.takeIf { it.path.isNotEmpty() }

    @Input
    override fun getOutJarFilters(): MutableList<Any?> = super.getOutJarFilters()

    @Input
    override fun getInJarFilters(): MutableList<Any?> = super.getInJarFilters()

    @Input
    override fun getLibraryJarFilters(): MutableList<Any?> = super.getLibraryJarFilters()

    @Internal
    override fun getOutJarFiles(): MutableList<Any?> = super.getOutJarFiles()

    @Internal
    override fun getInJarFiles(): MutableList<Any?> = super.getInJarFiles()

    @Internal
    override fun getInJarCounts(): MutableList<Any?> = super.getInJarCounts()

    @Internal
    override fun getLibraryJarFiles(): MutableList<Any?> = super.getLibraryJarFiles()

    /*
     * Inputs properly declared these methods so we don't override them
     *
     * configuration(configurationFiles: Any?)
     * libraryjars(libraryJarFiles: Any?)
     * libraryjars(filterArgs: MutableMap<Any?, Any?>?, libraryJarFiles: Any?)
     * injars(inJarFiles: Any?)
     * injars(filterArgs: MutableMap<Any?, Any?>?, inJarFiles: Any?)
     * outjars(outJarFiles: Any?)
     * outjars(filterArgs: MutableMap<Any?, Any?>?, outJarFiles: Any?)
     * printconfiguration()
     * printconfiguration(printConfiguration: Any?)
    */

    override fun renamesourcefileattribute() = throw NotImplementedError()

    override fun renamesourcefileattribute(newSourceFileAttribute: String?) = throw NotImplementedError()

    override fun dontshrink() = throw NotImplementedError()

    override fun assumenosideeffects(classSpecificationString: String?) = throw NotImplementedError()

    override fun assumenosideeffects(classSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    override fun keepnames(classSpecificationString: String?) = throw NotImplementedError()

    override fun keepnames(keepArgs: MutableMap<Any?, Any?>?, classSpecificationString: String?) = throw NotImplementedError()

    override fun keepnames(keepClassSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    override fun keepnames(keepClassSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    override fun printmapping() = throw NotImplementedError()

    override fun printmapping(printMapping: Any?) = throw NotImplementedError()

    override fun keep(classSpecificationString: String?) = throw NotImplementedError()

    override fun keep(keepArgs: MutableMap<Any?, Any?>?, classSpecificationString: String?) = throw NotImplementedError()

    override fun keep(keepClassSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    override fun keep(keepClassSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    override fun keepdirectories() = throw NotImplementedError()

    override fun keepdirectories(filter: String?) = throw NotImplementedError()

    override fun dontpreverify() = throw NotImplementedError()

    override fun dontnote() = throw NotImplementedError()

    override fun dontnote(filter: String?) = throw NotImplementedError()

    @Internal
    override fun getrenamesourcefileattribute(): Any? = throw NotImplementedError()

    override fun useuniqueclassmembernames() = throw NotImplementedError()

    override fun overloadaggressively() = throw NotImplementedError()

    @Internal
    override fun getprintusage(): Any? = throw NotImplementedError()

    @Internal
    override fun getforceprocessing(): Any? = throw NotImplementedError()

    override fun whyareyoukeeping(classSpecificationString: String?) = throw NotImplementedError()

    override fun whyareyoukeeping(classSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    override fun whyareyoukeeping(classSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    override fun obfuscationdictionary(obfuscationDictionary: Any?) = throw NotImplementedError()

    override fun adaptclassstrings() = throw NotImplementedError()

    override fun adaptclassstrings(filter: String?) = throw NotImplementedError()

    override fun applymapping(applyMapping: Any?) = throw NotImplementedError()

    override fun mergeinterfacesaggressively() = throw NotImplementedError()

    @Internal
    override fun getdontwarn(): Any? = throw NotImplementedError()

    override fun ignorewarnings() = throw NotImplementedError()

    @Internal
    override fun getaddconfigurationdebugging(): Any? = throw NotImplementedError()

    override fun field(memberSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    @Internal
    override fun getallowaccessmodification(): Any? = throw NotImplementedError()

    override fun constructor(memberSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    override fun dontusemixedcaseclassnames() = throw NotImplementedError()

    @Internal
    override fun getignorewarnings(): Any? = throw NotImplementedError()

    @Internal
    override fun getkeepdirectories(): Any? = throw NotImplementedError()

    override fun classobfuscationdictionary(classObfuscationDictionary: Any?) = throw NotImplementedError()

    override fun verbose() = throw NotImplementedError()

    override fun optimizations(filter: String?) = throw NotImplementedError()

    @Internal
    override fun getuseuniqueclassmembernames(): Any? = throw NotImplementedError()

    @Internal
    override fun getmicroedition(): Any? = throw NotImplementedError()

    override fun assumenoescapingparameters(classSpecificationString: String?) = throw NotImplementedError()

    override fun assumenoescapingparameters(classSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    @Internal
    override fun getandroid(): Any? = throw NotImplementedError()

    override fun keeppackagenames() = throw NotImplementedError()

    override fun keeppackagenames(filter: String?) = throw NotImplementedError()

    @Internal
    override fun getoverloadaggressively(): Any? = throw NotImplementedError()

    override fun skipnonpubliclibraryclasses() = throw NotImplementedError()

    @Internal
    override fun getdontusemixedcaseclassnames(): Any? = throw NotImplementedError()

    @Internal
    override fun getdontnote(): Any? = throw NotImplementedError()

    override fun assumenoexternalreturnvalues(classSpecificationString: String?) = throw NotImplementedError()

    override fun assumenoexternalreturnvalues(classSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    override fun target(targetClassVersion: String?) = throw NotImplementedError()

    override fun keepattributes() = throw NotImplementedError()

    override fun keepattributes(filter: String?) = throw NotImplementedError()

    override fun keepclassmembernames(classSpecificationString: String?) = throw NotImplementedError()

    override fun keepclassmembernames(keepArgs: MutableMap<Any?, Any?>?, classSpecificationString: String?) = throw NotImplementedError()

    override fun keepclassmembernames(keepClassSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    override fun keepclassmembernames(keepClassSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    @Internal
    override fun getdontpreverify(): Any? = throw NotImplementedError()

    @Internal
    override fun getverbose(): Any? = throw NotImplementedError()

    @Internal
    override fun getskipnonpubliclibraryclasses(): Any? = throw NotImplementedError()

    @Internal
    override fun getdontoptimize(): Any? = throw NotImplementedError()

    override fun keepclasseswithmembernames(classSpecificationString: String?) = throw NotImplementedError()

    override fun keepclasseswithmembernames(keepArgs: MutableMap<Any?, Any?>?, classSpecificationString: String?) = throw NotImplementedError()

    override fun keepclasseswithmembernames(keepClassSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    override fun keepclasseswithmembernames(keepClassSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    override fun keepclasseswithmembers(classSpecificationString: String?) = throw NotImplementedError()

    override fun keepclasseswithmembers(keepArgs: MutableMap<Any?, Any?>?, classSpecificationString: String?) = throw NotImplementedError()

    override fun keepclasseswithmembers(keepClassSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    override fun keepclasseswithmembers(keepClassSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    @Internal
    override fun getdump(): Any? = throw NotImplementedError()

    override fun printseeds() = throw NotImplementedError()

    override fun printseeds(printSeeds: Any?) = throw NotImplementedError()

    override fun dontoptimize() = throw NotImplementedError()

    override fun dontobfuscate() = throw NotImplementedError()

    override fun extendClassSpecifications(
        classSpecifications: MutableList<Any?>?,
        classSpecification: ClassSpecification?
    ): MutableList<Any?> = throw NotImplementedError()

    override fun allowaccessmodification() = throw NotImplementedError()

    @Internal
    override fun getdontobfuscate(): Any? = throw NotImplementedError()

    @Internal
    override fun getprintmapping(): Any? = throw NotImplementedError()

    override fun flattenpackagehierarchy() = throw NotImplementedError()

    override fun flattenpackagehierarchy(flattenPackageHierarchy: String?) = throw NotImplementedError()

    override fun android() = throw NotImplementedError()

    override fun dump() = throw NotImplementedError()

    override fun dump(dump: Any?) = throw NotImplementedError()

    @Internal
    override fun getdontshrink(): Any? = throw NotImplementedError()

    @Internal
    override fun getkeepattributes(): Any? = throw NotImplementedError()

    override fun microedition() = throw NotImplementedError()

    override fun keepparameternames() = throw NotImplementedError()

    override fun addconfigurationdebugging() = throw NotImplementedError()

    override fun packageobfuscationdictionary(packageObfuscationDictionary: Any?) = throw NotImplementedError()

    @Internal
    override fun getdontskipnonpubliclibraryclassmembers(): Any? = throw NotImplementedError()

    override fun dontskipnonpubliclibraryclassmembers() = throw NotImplementedError()

    @Internal
    override fun getprintconfiguration(): Any? = throw NotImplementedError()

    override fun forceprocessing() = throw NotImplementedError()

    override fun keepclassmembers(classSpecificationString: String?) = throw NotImplementedError()

    override fun keepclassmembers(keepArgs: MutableMap<Any?, Any?>?, classSpecificationString: String?) = throw NotImplementedError()

    override fun keepclassmembers(keepClassSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    override fun keepclassmembers(keepClassSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    @Internal
    override fun getmergeinterfacesaggressively(): Any? = throw NotImplementedError()

    @Internal
    override fun getConfigurationFiles(): MutableList<Any?> = throw NotImplementedError()

    @Internal
    override fun getkeeppackagenames(): Any? = throw NotImplementedError()

    override fun assumevalues(classSpecificationString: String?) = throw NotImplementedError()

    override fun assumevalues(classSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    override fun printusage() = throw NotImplementedError()

    override fun printusage(printUsage: Any?) = throw NotImplementedError()

    @Internal
    override fun getprintseeds(): Any? = throw NotImplementedError()

    @Internal
    override fun getadaptresourcefilenames(): Any? = throw NotImplementedError()

    override fun assumenoexternalsideeffects(classSpecificationString: String?) = throw NotImplementedError()

    override fun assumenoexternalsideeffects(classSpecificationArgs: MutableMap<Any?, Any?>?, classMembersClosure: Closure<*>?) = throw NotImplementedError()

    override fun dontwarn() = throw NotImplementedError()

    override fun dontwarn(filter: String?) = throw NotImplementedError()

    @Internal
    override fun getrepackageclasses(): Any? = throw NotImplementedError()

    @Internal
    override fun getadaptresourcefilecontents(): Any? = throw NotImplementedError()

    @Internal
    override fun getflattenpackagehierarchy(): Any? = throw NotImplementedError()

    override fun optimizationpasses(optimizationPasses: Int) = throw NotImplementedError()

    override fun adaptresourcefilenames() = throw NotImplementedError()

    override fun adaptresourcefilenames(filter: String?) = throw NotImplementedError()

    override fun method(memberSpecificationArgs: MutableMap<Any?, Any?>?) = throw NotImplementedError()

    @Internal
    override fun getadaptclassstrings(): Any? = throw NotImplementedError()

    override fun repackageclasses() = throw NotImplementedError()

    override fun repackageclasses(repackageClasses: String?) = throw NotImplementedError()

    @Internal
    override fun getkeepparameternames(): Any? = throw NotImplementedError()

    override fun adaptresourcefilecontents() = throw NotImplementedError()

    override fun adaptresourcefilecontents(filter: String?) = throw NotImplementedError()
}
