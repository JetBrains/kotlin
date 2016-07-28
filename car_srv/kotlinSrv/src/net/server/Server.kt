package server


import require
import server.handlers.AbstractHandler

/**
 * Created by user on 7/27/16.
 */
class Server(handlers: MutableMap<String, AbstractHandler>) {

    val http: dynamic
    val url: dynamic
    val handlers: MutableMap<String, AbstractHandler>

    init {
        http = require("http")
        url = require("url")
        this.handlers = handlers
    }


    fun start() {

        http.createServer({ request, response ->
            val content = mutableListOf<Byte>()
            val urlName = url.parse(request.url).pathname;
            val handler = handlers.get(urlName)
            request.on("data", {
                data ->
                for (i in 0..data.length - 1) {
                    content.add(data[i])
                }
            })
            request.on("end", {
                if (handler != null) {
                    handler.execute(content, response)
                } else {
                    //todo write error on incorrect url
                    response.end()
                }
            })
        }).listen(8888)
    }

}