abstract class Introspector<M : Model>(protected val model: Model) {
    protected abstract inner class Retriever(protected val transaction: Any) {
        protected var model: Model = this@Introspector.model
    }

    protected abstract inner class SchemaRetriever(transaction: Any): Retriever(transaction) {
        protected inline fun inSchema(crossinline modifier: (Any) -> Unit) =
                model.modify { schema -> modifier.invoke(schema) }
    }
}
