-dontnote **

-target 1.6
-dontoptimize
-dontobfuscate
# -dontshrink

-keep public class kotlin.reflect.* { *; }
-keep public class kotlin.reflect.jvm.* { *; }
-keep public class kotlin.reflect.full.* { *; }

-keepattributes SourceFile,LineNumberTable,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod

-keep class kotlin.reflect.jvm.internal.ReflectionFactoryImpl { public protected *; }

-keep class * implements kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition { public protected *; }
-keep class * implements kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader { public protected *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    ** toString();
}

# For tests on HashPMap, see compiler/testData/codegen/box/hashPMap
-keepclassmembers class kotlin.reflect.jvm.internal.pcollections.HashPMap {
    public int size();
    public boolean containsKey(java.lang.Object);
    public kotlin.reflect.jvm.internal.pcollections.HashPMap minus(java.lang.Object);
}

# This is needed because otherwise ProGuard strips generic signature of this class (even though we pass `-keepattributes Signature` above)
# See KT-23962 and https://sourceforge.net/p/proguard/bugs/482/
-keep class kotlin.reflect.jvm.internal.impl.protobuf.GeneratedMessageLite$ExtendableMessageOrBuilder
