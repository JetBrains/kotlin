// FIR_IDENTICAL
class KotlinSharedNativeCompilation() : KotlinMetadataCompilation<KotlinCommonOptions>, AbstractKotlinCompilation<KotlinCommonOptions>()

interface KotlinCommonOptions

interface KotlinMetadataCompilation<T : KotlinCommonOptions> : KotlinCompilation<T>

interface KotlinCompilation<out T : KotlinCommonOptions>

class KotlinCommonCompilation : KotlinMetadataCompilation<KotlinMultiplatformCommonOptions>, AbstractKotlinCompilation<KotlinMultiplatformCommonOptions>()

interface KotlinMultiplatformCommonOptions  : KotlinCommonOptions

abstract class AbstractKotlinCompilation<T : KotlinCommonOptions> : KotlinCompilation<T>

fun main() {
    val compilation = when {
        true -> {
            KotlinSharedNativeCompilation()
        }
        else -> KotlinCommonCompilation()
    }
}
