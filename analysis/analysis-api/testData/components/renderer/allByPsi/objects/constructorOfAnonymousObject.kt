interface IFace<T> {
    fun getStatus(arg: T): Boolean
}
class Some

private fun resolve(): IFace<Some> {
    return object : IFace<Some> {
        override fun getStatus(arg: Some) = true
    }
}