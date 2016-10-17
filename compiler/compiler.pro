-injars '<output>/kotlin-compiler-before-shrink.jar'(
!com/thoughtworks/xstream/converters/extended/ISO8601**,
!com/thoughtworks/xstream/converters/reflection/CGLIBEnhancedConverter**,
!com/thoughtworks/xstream/io/xml/JDom**,
!com/thoughtworks/xstream/io/xml/Dom4J**,
!com/thoughtworks/xstream/io/xml/Xom**,
!com/thoughtworks/xstream/io/xml/Wstx**,
!com/thoughtworks/xstream/io/xml/KXml2**,
!com/thoughtworks/xstream/io/xml/BEAStax**,
!com/thoughtworks/xstream/io/json/Jettison**,
!com/thoughtworks/xstream/mapper/CGLIBMapper**,
!com/thoughtworks/xstream/mapper/LambdaMapper**,
!org/apache/log4j/jmx/Agent*,
!org/apache/log4j/net/JMS*,
!org/apache/log4j/net/SMTP*,
!org/apache/log4j/or/jms/MessageRenderer*,
!org/jdom/xpath/Jaxen*,
!org/mozilla/javascript/xml/impl/xmlbeans/**,
!net/sf/cglib/**,
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
-dontwarn com.intellij.psi.util.PsiClassUtil
-dontwarn org.apache.hadoop.io.compress.*
-dontwarn com.google.j2objc.annotations.Weak
-dontwarn org.iq80.snappy.HadoopSnappyCodec$SnappyCompressionInputStream
-dontwarn org.iq80.snappy.HadoopSnappyCodec$SnappyCompressionOutputStream
-dontwarn com.google.common.util.concurrent.*

-libraryjars '<rtjar>'
-libraryjars '<jssejar>'
-libraryjars '<bootstrap.runtime>'
-libraryjars '<bootstrap.reflect>'
-libraryjars '<bootstrap.script.runtime>'

-target 1.6
-dontoptimize
-dontobfuscate

-keep class org.fusesource.** { *; }
-keep class com.sun.jna.** { *; }

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

-keep class org.jetbrains.kotlin.protobuf.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.container.** { *; }

-keep class org.jetbrains.kotlin.codegen.intrinsics.IntrinsicArrayConstructorsKt { *; }

-keep class org.jetbrains.org.objectweb.asm.Opcodes { *; }

-keep class org.jetbrains.kotlin.codegen.extensions.** {
    public protected *;
}

-keepclassmembers class com.intellij.openapi.vfs.VirtualFile {
    public protected *;
}

-keep class com.intellij.openapi.vfs.StandardFileSystems {
    public static *;
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

# This is needed so that the platform code which parses XML wouldn't fail, see KT-16968
# This API is used from org.jdom.input.SAXBuilder via reflection.
-keep class org.jdom.input.JAXPParserFactory { public ** createParser(...); }

# for kdoc & dokka
-keep class com.intellij.openapi.util.TextRange { *; }
-keep class com.intellij.lang.impl.PsiBuilderImpl* {
    public protected *;
}
-keep class com.intellij.openapi.util.text.StringHash { *; }

# for j2k
-keep class com.intellij.codeInsight.NullableNotNullManager { public protected *; }

# for gradle (see KT-12549)
-keep class com.intellij.lang.properties.charset.Native2AsciiCharsetProvider { *; }

# for kotlin-build-common (consider repacking compiler together with kotlin-build-common and remove this part afterwards)
-keep class com.intellij.util.io.IOUtil { public *; }
-keep class com.intellij.openapi.util.io.FileUtil { public *; }
-keep class com.intellij.util.SystemProperties { public *; }
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

-keep class org.jetbrains.org.objectweb.asm.tree.AnnotationNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.ClassNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.LocalVariableNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.MethodNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.FieldNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.ParameterNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.TypeAnnotationNode { *; }

-keep class org.jetbrains.org.objectweb.asm.signature.SignatureReader { *; }
-keep class org.jetbrains.org.objectweb.asm.signature.SignatureVisitor { *; }

-keepclassmembers class org.jetbrains.org.objectweb.asm.Type {
    *** ARRAY;
    *** OBJECT;
}

-keepclassmembers class org.jetbrains.org.objectweb.asm.ClassReader {
    *** SKIP_CODE;
    *** SKIP_DEBUG;
    *** SKIP_FRAMES;
}

# for kotlin-android-extensions in maven
-keep class com.intellij.openapi.module.ModuleServiceManager { public *; }

# for building kotlin-build-common-test
-keep class org.jetbrains.kotlin.build.SerializationUtilsKt { *; }
