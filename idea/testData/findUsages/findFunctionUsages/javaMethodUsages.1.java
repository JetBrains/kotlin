import testing.*;

class Client {
    public void foo() {
        new Server().processRequest();
        new ServerEx().processRequest();
    }
}
