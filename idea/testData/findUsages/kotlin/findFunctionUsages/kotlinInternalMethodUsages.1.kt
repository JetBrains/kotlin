import server.*;

class Client {
    public fun foo() {
        Server().processRequest()
        ServerEx().processRequest()
    }
}
