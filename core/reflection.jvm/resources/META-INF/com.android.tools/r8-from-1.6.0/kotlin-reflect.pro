# When editing this file, update the following files as well:
# - META-INF/com.android.tools/proguard/kotlin-reflect.pro
# - META-INF/com.android.tools/r8-upto-1.6.0/kotlin-reflect.pro
# - META-INF/proguard/kotlin-reflect.pro
# Keep Metadata annotations so they can be parsed at runtime.
-keep class kotlin.Metadata { *; }

# Ensure that known types to reflect and kotlinc are not rewritten to object in the metadata if pruned
-keep,allowshrinking,allowoptimization kotlin.Unit
-keep,allowshrinking,allowoptimization kotlin.UByte
-keep,allowshrinking,allowoptimization kotlin.UByteArray
-keep,allowshrinking,allowoptimization kotlin.UShort
-keep,allowshrinking,allowoptimization kotlin.UShortArray
-keep,allowshrinking,allowoptimization kotlin.UInt
-keep,allowshrinking,allowoptimization kotlin.UIntArray
-keep,allowshrinking,allowoptimization kotlin.ULong
-keep,allowshrinking,allowoptimization kotlin.ULongArray
-keep,allowshrinking,allowoptimization kotlin.Function

# Keep generic signatures and annotations at runtime.
# R8 requires InnerClasses and EnclosingMethod if you keepattributes Signature.
-keepattributes InnerClasses,Signature,RuntimeVisible*Annotations,EnclosingMethod

# Don't note on API calls from different JVM versions as they're gated properly at runtime.
-dontnote kotlin.internal.PlatformImplementationsKt

# Don't note on internal APIs, as there is some class relocating that shrinkers may unnecessarily find suspicious.
-dontwarn kotlin.reflect.jvm.internal.**

