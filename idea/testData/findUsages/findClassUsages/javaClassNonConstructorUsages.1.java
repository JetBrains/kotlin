import testing.*;

class Client {
    private Server myServer = new Server();

    public void setServer(Server server) {
        myServer = server;
    }

    public Server getMyServer() {
        return myServer;
    }
}
