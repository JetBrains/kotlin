// DISABLE-ERRORS
interface A<in TPipeline, out TBuilder : Any, TFeature : Any> {
    fun install(pipeline: TPipeline, configure: TBuilder.() -> Unit): TFeature
}

class X : A<String, UnresolvedType, Unit> {
    <caret>
}