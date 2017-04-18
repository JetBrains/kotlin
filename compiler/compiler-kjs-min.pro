-injars '<output>/kotlin-compiler-before-shrink-min.jar'(
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

-injars '<bootstrap.runtime>'
-injars '<bootstrap.script.runtime>'

-outjars '<kotlin-home>/lib/kotlin-compiler-min.jar'

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
-dontwarn org.apache.xerces.dom.**
-dontwarn org.apache.xerces.util.**
-dontwarn org.w3c.dom.ElementTraversal
-dontwarn javaslang.match.annotation.Unapply
-dontwarn javaslang.match.annotation.Patterns

-dontwarn kotlin.reflect.**


-libraryjars '<rtjar>'
-libraryjars '<jssejar>'
#-libraryjars '<bootstrap.runtime>'
#-libraryjars '<bootstrap.reflect>'
#-libraryjars '<bootstrap.script.runtime>'

-dontoptimize
-dontobfuscate

#-keep class org.jetbrains.annotations.** {
#    public protected *;
#}
#
-keep class javax.inject.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.psi.** {
    public protected *;
}
-keep class org.jetbrains.kotlin.js.** {
    public protected *;
}
-keep class org.jetbrains.kotlin.** {
    public protected *;
}

#-keep class org.jetbrains.kotlin.compiler.plugin.** {
#    public protected *;
#}
#
#-keep class org.jetbrains.kotlin.extensions.** {
#    public protected *;
#}
#
#-keep class org.jetbrains.kotlin.protobuf.** {
#    public protected *;
#}
#
#-keep class org.jetbrains.kotlin.container.** { *; }

#-keepclassmembers class com.intellij.openapi.vfs.VirtualFile {
#    public InputStream getInputStream();
#}
#
#-keep class com.intellij.openapi.vfs.StandardFileSystems {
#    public static *;
#}

#-keep class com.intellij.psi.** {
#    public protected *;
#}

# Used from org.jdom.input.SAXBuilder throught reflection
-keep class org.jdom.input.JAXPParserFactory { public ** createParser(...); }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    ** toString();
    ** hashCode();
    #void start();
    #void stop();
    #void dispose();
}

#-keep class org.jetbrains.kotlin.cli.js.K2JSCompiler {
#    public static void main(java.lang.String[]);
#}

#-keep class com.intellij.openapi.util.Disposer { public *; }
-keep class org.picocontainer.Disposable { public *; }
-keep class org.picocontainer.Startable { public *; }
#
#-keep class org.jetbrains.kotlin.config.KotlinSourceRoot { public *; }
#
#-keep class org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment { *; }
#-keep class org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles {
#    #public static ** JS_CONFIG_FILES;
#    *;
#}
#
#-keep class org.jetbrains.kotlin.js.config.JsConfig { public *; }
#-keep class org.jetbrains.kotlin.js.facade.K2JSTranslator { public *; }

#-keep class org.jetbrains.kotlin.config.JVMConfigurationKeys { public *;}

-keep class org.jetbrains.kotlin.js.cli.SimpleRunnerKt { public *;}
