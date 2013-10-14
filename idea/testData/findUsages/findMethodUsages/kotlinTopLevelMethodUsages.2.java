package client;

import server.ServerPackage;

class JClient {
    String s = ServerPackage.processRequest();

    String sendRequest() {
        return ServerPackage.processRequest();
    }
}