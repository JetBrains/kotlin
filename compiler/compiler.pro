-injars '<output>/kotlin-compiler-before-shrink.jar'(
!com/thoughtworks/xstream/converters/extended/ISO8601**,
!com/thoughtworks/xstream/converters/reflection/CGLIBEnhancedConverter**,
!com/thoughtworks/xstream/io/xml/Dom4J**,
!com/thoughtworks/xstream/io/xml/Xom**,
!com/thoughtworks/xstream/io/xml/Wstx**,
!com/thoughtworks/xstream/io/xml/KXml2**,
!com/thoughtworks/xstream/io/xml/BEAStax**,
!com/thoughtworks/xstream/io/json/Jettison**,
!com/thoughtworks/xstream/mapper/CGLIBMapper**,
!org/apache/log4j/jmx/Agent*,
!org/apache/log4j/net/JMS*,
!org/apache/log4j/net/SMTP*,
!org/apache/log4j/or/jms/MessageRenderer*,
!org/jdom/xpath/Jaxen*,
!org/mozilla/javascript/xml/impl/xmlbeans/**,
!META-INF/maven**,
**.class,**.properties,**.kt,**.kotlin_*,**.jnilib,**.so,**.dll,
META-INF/services/**,META-INF/native/**,META-INF/extensions/**,META-INF/MANIFEST.MF,
messages/**)

-outjars '<kotlin-home>/lib/kotlin-compiler.jar'

-dontnote **
-dontwarn com.intellij.util.ui.IsRetina*
-dontwarn com.intellij.util.RetinaImage*
-dontwarn apple.awt.*
-dontwarn dk.brics.automaton.*
-dontwarn org.fusesource.**
-dontwarn org.imgscalr.Scalr**
-dontwarn org.xerial.snappy.SnappyBundleActivator
-dontwarn com.intellij.util.CompressionUtil
-dontwarn com.intellij.util.SnappyInitializer
-dontwarn com.intellij.util.SVGLoader
-dontwarn com.intellij.util.SVGLoader$MyTranscoder
-dontwarn net.sf.cglib.**
-dontwarn org.objectweb.asm.** # this is ASM3, the old version that we do not use
-dontwarn com.sun.jna.NativeString
-dontwarn com.sun.jna.WString

-libraryjars '<rtjar>'
-libraryjars '<jssejar>'
-libraryjars '<bootstrap.runtime>'
-libraryjars '<bootstrap.reflect>'

-target 1.6
-dontoptimize
-dontobfuscate

-keep class org.fusesource.** { *; }
-keep class com.sun.jna.** { *; }
-keep class org.jdom.input.JAXPParserFactory { *; }

-keep class org.jetbrains.annotations.** {
    public protected *;
}

-keep class javax.inject.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.compiler.plugin.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.extensions.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.codegen.intrinsics.IntrinsicArrayConstructorsKt { *; }

-keep class org.jetbrains.org.objectweb.asm.Opcodes { *; }

-keep class org.jetbrains.kotlin.codegen.extensions.** {
    public protected *;
}

-keepclassmembers class com.intellij.openapi.vfs.VirtualFile {
    public InputStream getInputStream();
}

# needed for jar cache cleanup in the gradle plugin and compile daemon
-keepclassmembers class com.intellij.openapi.vfs.impl.ZipHandler {
    public static void clearFileAccessorCache();
}

-keep class jet.** {
    public protected *;
}

-keep class com.intellij.psi.** {
    public protected *;
}

# for kdoc & dokka
-keep class com.intellij.openapi.util.TextRange { *; }
-keep class com.intellij.lang.impl.PsiBuilderImpl* {
    public protected *;
}
-keep class com.intellij.openapi.util.text.StringHash { *; }

# for gradle plugin and other server tools
-keep class com.intellij.openapi.util.io.ZipFileCache { public *; }

# for j2k
-keep class com.intellij.codeInsight.NullableNotNullManager { public protected *; }

# for kotlin-build-common (consider repacking compiler together with kotlin-build-common and remove this part afterwards)
-keep class com.intellij.util.io.IOUtil { public *; }
-keep class com.intellij.openapi.util.io.FileUtil { public *; }
-keep class com.intellij.util.SystemProperties { public *; }
-keep class jsr166e.extra.SequenceLock { *; }
-keep class com.intellij.util.containers.hash.LinkedHashMap { *; }
-keep class com.intellij.util.containers.ConcurrentIntObjectMap { *; }
-keep class com.intellij.util.containers.ComparatorUtil { *; }
-keep class com.intellij.util.io.PersistentHashMapValueStorage { *; }
-keep class com.intellij.util.io.PersistentHashMap { *; }
-keep class com.intellij.util.io.BooleanDataDescriptor { *; }
-keep class com.intellij.util.io.EnumeratorStringDescriptor { *; }
-keep class com.intellij.util.io.ExternalIntegerKeyDescriptor { *; }
-keep class com.intellij.util.containers.hash.EqualityPolicy { *; }
-keep class com.intellij.util.containers.hash.EqualityPolicy.* { *; }
-keep class com.intellij.util.containers.Interner { *; }
-keep class gnu.trove.TIntHashSet { *; }
-keep class gnu.trove.TIntIterator { *; }
-keep class org.iq80.snappy.SlowMemory { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    ** toString();
    ** hashCode();
    void start();
    void stop();
    void dispose();
}

-keepclassmembers class org.jetbrains.org.objectweb.asm.Opcodes {
    *** ASM5;
}

-keepclassmembers class org.jetbrains.org.objectweb.asm.ClassReader {
    *** SKIP_CODE;
    *** SKIP_DEBUG;
    *** SKIP_FRAMES;
}
