import server.*;

class Client {
    public fun foo() {
        println(Server(foo = "!").foo)
        ServerEx().processRequest()
    }
}
