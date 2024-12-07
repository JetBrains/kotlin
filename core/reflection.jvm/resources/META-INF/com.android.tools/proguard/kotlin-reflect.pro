# When editing this file, update the following files as well:
# - META-INF/com.android.tools/r8-from-1.6.0/kotlin-reflect.pro
# - META-INF/com.android.tools/r8-upto-1.6.0/kotlin-reflect.pro
# - META-INF/proguard/kotlin-reflect.pro
# Keep Metadata annotations so they can be parsed at runtime.
-keep class kotlin.Metadata { *; }

# Keep implementations of service loaded interfaces
-keep interface kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader
-keep class * implements kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader { public protected *; }
-keep interface kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition
-keep class * implements kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition { public protected *; }

# Keep generic signatures and annotations at runtime.
-keepattributes Signature,RuntimeVisible*Annotations

# Don't note on API calls from different JVM versions as they're gated properly at runtime.
-dontnote kotlin.internal.PlatformImplementationsKt

# Don't note on internal APIs, as there is some class relocating that shrinkers may unnecessarily find suspicious.
-dontwarn kotlin.reflect.jvm.internal.**

# Statically guarded by try-catch block and not used on Android, see CacheByClass
-dontwarn java.lang.ClassValue
