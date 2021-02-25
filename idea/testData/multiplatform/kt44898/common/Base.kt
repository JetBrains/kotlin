package sample

public abstract class <!LINE_MARKER("descr='Is subclassed by Concrete'")!>Base<!> : I {
    override suspend fun <A : Appendable> <!LINE_MARKER("descr='Implements function in 'I''")!>readUTF8LineTo<!>(out: A, limit: Int): Boolean {
        TODO("Not yet implemented")
    }
}