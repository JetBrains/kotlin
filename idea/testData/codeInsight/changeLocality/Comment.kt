// SCOPE: '// some change'

class Comment {
    fun q() {

    }

    val someValue: String
        get() {
            return "X"
        }

    fun baa() {
        <selection>// some change</selection>
        val list = listOf<String>(TODO(""))
    }
}