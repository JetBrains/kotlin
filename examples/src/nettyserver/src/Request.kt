package org.jetbrains.kotlin.examples.netty

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import java.util.LinkedList
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.HttpResponseStatus.*
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.handler.codec.http.HttpVersion
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import org.jboss.netty.handler.codec.http.HttpHeaders.*
import org.jboss.netty.handler.codec.http.HttpMessage
import org.jboss.netty.channel.ChannelFuture

class RequestResponse(e: MessageEvent) {
    val request  = e.getMessage() as HttpRequest
    val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN)
    val channel  = e.getChannel()!!
    val path     = request.getUri()!!.sanitizeUri()

    fun setError(status: HttpResponseStatus?) {
        response.setStatus(status)
        response["Content-Type"] = "text/plain; charset=UTF-8"
        response.content = "Failure: " + status.toString() + "\r\n"
    }

    fun redirect(path: String) {
        response.setStatus(HttpResponseStatus.MOVED_PERMANENTLY)
        response["Location"] = path
        response["Content-Length"] = 0
        channel.write(response)
    }

    fun ok() : RequestResponse {
        response.setStatus(HttpResponseStatus.OK)
        return this
    }

    fun write() =
        if(response.getStatus()!!.getCode() >= 400) {
            channel.write(response)!!.addListener(ChannelFutureListener.CLOSE)
        }
        else {
            channel.write(response)
        }
}


fun String.sanitizeUri() : String {
    val path = decodeURI("UTF-8") ?: decodeURI("ISO-8859-1") ?: throw Error()

    val localizedPath = path.replace('/', File.separatorChar)

    return if (localizedPath.contains(File.separator + ".") ||
        localizedPath.contains("." + File.separator) ||
        localizedPath.startsWith(".") || localizedPath.endsWith("."))
            throw Error()
        else
            localizedPath
}

fun String.decodeURI(encoding : String) : String? {
    try {
        return URLDecoder.decode(this, encoding)
    }
    catch (e : UnsupportedEncodingException) {
        return null
    }
}

trait Processor {
    fun tryToProcess(request: RequestResponse) : Boolean
}

