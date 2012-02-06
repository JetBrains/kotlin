package org.jetbrains.kotlin.examples.netty

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import java.util.LinkedList
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.HttpResponseStatus.*
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpResponseStatus

class HttpHandler(config: HttpHandler.() -> Unit) : SimpleChannelUpstreamHandler() {
    private var processors = LinkedList<Processor>();

    var onNotFound : (RequestResponse)->Any? =   { request -> request.setError(NOT_FOUND) };

    {
        this.bind(config)()
    }

    fun messageReceived(ctx : ChannelHandlerContext, e : MessageEvent) {
        val request = RequestResponse(e)
        for(processor in processors) {
            if(processor.tryToProcess(request)) {
                request.write()
                return
            }
        }

        (onNotFound)(request)
    }

    fun static(prefix: String = "/", directory: String) {
        processors.add(StaticFileProcessor(prefix, directory))
    }

    fun rest(prefix: String, config: RestBuilder.()->Any?) {
        val builder = RestBuilder()
        (builder.bind(config))()
        processors.add(RestProcessor(prefix, builder))
    }
}
