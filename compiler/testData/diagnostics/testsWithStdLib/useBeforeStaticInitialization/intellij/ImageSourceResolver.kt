// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// WITH_PLATFORM_LIBS
public interface ImageSourceResolver {
    /**
     * Resolves a raw image destination string from a Markdown file into a fully-qualified, loadable path.
     *
     * @param rawDestination The raw destination string from the Markdown, e.g., "my-image.png" or
     *   "https://example.com/image.png".
     * @return A fully-qualified, loadable path to the image, which can be consumed by an image loader, or `null` if the
     *   image could not be resolved.
     */
    public fun resolve(rawDestination: String): String?

    public companion object {
        internal val defaultCapabilities =
            setOf(
                ResolveCapability.PlainUri,
                ResolveCapability.RelativePathInResources(),
                ResolveCapability.AbsolutePath,
            )

        /**
         * Creates [ImageSourceResolver] that can resolve image links in Markdown files if they are either:
         * - plain URIs, e.g., `https://example.com/image.png` or `file:///image.png`
         * - absolute paths, e.g., `/image.png`
         * - relative paths in the current classloader's resources, e.g., `/images/my-image.png`
         * - relative paths relative to a given root directory [rootDir], e.g., `../images/my-image.png`
         *
         * If [logResolveFailure] is true, logs any failures to resolve image sources.
         */
        public fun create(rootDir: String, logResolveFailure: Boolean): ImageSourceResolver =
            create(
                resolveCapabilities = buildSet {
                    addAll(defaultCapabilities)
                    add(ResolveCapability.RelativePath(rootDir))
                },
                logResolveFailure = logResolveFailure,
            )

        /**
         * Creates [ImageSourceResolver] that can resolve image links in Markdown files according to provided
         * [resolveCapabilities].
         *
         * If [logResolveFailure] is true, logs any failures to resolve image sources.
         */
        public fun create(
            resolveCapabilities: Set<ResolveCapability> = defaultCapabilities,
            logResolveFailure: Boolean = true,
        ): ImageSourceResolver = DefaultImageSourceResolver(resolveCapabilities, logResolveFailure)
    }

    /** Provides a list of capabilities that the default [ImageSourceResolver] implementation supports. */
    public sealed interface ResolveCapability {
        /** Resolves a raw image destination string from a Markdown file into a fully-qualified, loadable path. */
        public fun resolve(rawDestination: String): String?

        /** Represents the ability to resolve plain URIs as-is. */
        public object PlainUri : ResolveCapability {
            override fun toString(): String = "PlainUri"

            override fun resolve(rawDestination: String): String? {
                return null
            }
        }

        /**
         * Represents the ability to resolve relative paths in the [resourceClass] classloader's resources, or in the
         * current classloader's resources if [resourceClass] is `null`.
         */
        public class RelativePathInResources(private val resourceClass: Class<*>? = null) : ResolveCapability {
            override fun toString(): String = "RelativePathInResources"

            override fun resolve(rawDestination: String): String? =
                (resourceClass ?: javaClass).classLoader.getResource(rawDestination.removePrefix("/"))?.toExternalForm()
        }

        /** Represents the ability to resolve absolute paths as-is. */
        public object AbsolutePath : ResolveCapability {
            override fun resolve(rawDestination: String): String? {
                return null
            }
        }

        /** Represents the ability to resolve relative paths relative to a given root directory [rootDir]. */
        public class RelativePath(private val rootDir: String) : ResolveCapability {
            override fun resolve(rawDestination: String): String? {
                return null
            }

            override fun toString(): String = "RelativePath(rootDir=$rootDir)"
        }
    }
}

/**
 * The default implementation of [ImageSourceResolver] that can resolve image links in Markdown files according to
 * provided [resolveCapabilities].
 *
 * @param resolveCapabilities A list of [ImageSourceResolver.ResolveCapability]s that this resolver can support.
 * @param logResolveFailure Whether to log any failures to resolve image sources.
 * @see ImageSourceResolver
 */
internal class DefaultImageSourceResolver(
    private val resolveCapabilities: Set<ImageSourceResolver.ResolveCapability> =
        ImageSourceResolver.defaultCapabilities,
    private val logResolveFailure: Boolean = true,
) : ImageSourceResolver {
    override fun resolve(rawDestination: String): String? {
        val result = resolveCapabilities.firstNotNullOfOrNull { it.resolve(rawDestination) }
        return result
    }
}

/**
 * Provides an [ImageSourceResolver] to the composition. You can use this to customize how image sources are resolved in
 * Markdown. You can use this API to resolve images from different classloaders or sources.
 *
 * For example, to resolve images relative to a base URL, you could provide an implementation like this:
 * ```kotlin
 * val baseUrl = "https://example.com/images/"
 * val resolver = object : ImageSourceResolver {
 *     override fun resolve(rawDestination: String): String {
 *         return baseUrl + rawDestination
 *     }
 * }
 *
 * CompositionLocalProvider(LocalMarkdownImageSourceResolver provides resolver) {
 *     MarkdownViewer(markdownText)
 * }
 * ```
 *
 * @see ImageSourceResolver
 * @see DefaultImageSourceResolver
 */
public val LocalMarkdownImageSourceResolver: () -> ImageSourceResolver = {
    ImageSourceResolver.create()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, elvisExpression, flexibleType, functionDeclaration,
functionalType, ifExpression, interfaceDeclaration, javaFunction, javaProperty, lambdaLiteral, localProperty,
nestedClass, nullableType, objectDeclaration, outProjection, override, primaryConstructor, propertyDeclaration, safeCall,
sealed, starProjection, stringLiteral */
