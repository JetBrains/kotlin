// IGNORE_BACKEND_FIR: JVM_IR
class Comment() {
    var article = ""
}

fun new(body: Comment.() -> Unit) : Comment {
    val c = Comment()
    c.body()
    return c
}

open class Request(val handler : Any.() -> Comment) {
    val s = handler().article
}


class A : Request ({
   new {
       this.article = "OK"
   }
})

fun box() : String {
    return A().s
}
