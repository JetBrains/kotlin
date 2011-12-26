import java.util.concurrent.Executors
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.io.*

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.channel.*
import org.jboss.netty.handler.codec.http.*
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.handler.codec.frame.TooLongFrameException
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.HttpResponseStatus.*

import netty.*
import jlstring.*

package jlstring {
    fun String.replace(c: Char, by: Char) : String = (this as java.lang.String).replace(c, by) as String

    fun String.contains(s: String) : Boolean = (this as java.lang.String).contains(s as java.lang.CharSequence)

    fun java.lang.String.plus(s: Any?) : String = (this as String) + s.toString()
}

package netty {
    fun ChannelPipeline.with(op: fun ChannelPipeline.() ) : ChannelPipeline {
        this.op()
        return this
    }

    fun HttpResponse.set(header: String, value: Any?) : Unit {
        setHeader(header, value)
    }

    fun HttpResponse.setContent(content: Any?) : Unit {
        setContent(ChannelBuffers.copiedBuffer(content.toString() as java.lang.String, CharsetUtil.UTF_8))
    }

    fun ChannelHandlerContext.sendError(status: HttpResponseStatus?) {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
        response["Content-Type"] = "text/plain; charset=UTF-8"
        response.setContent("Failure: " + status.toString() + "\r\n")

        // Close the connection as soon as the error message is sent.
        this.getChannel()?.write(response)?.addListener(ChannelFutureListener.CLOSE)
    }

    fun String.decodeURI(encoding : String) : String? {
        try {
            return URLDecoder.decode(this, encoding)
        }
        catch (e : UnsupportedEncodingException) {
            return null
        }
    }

    fun String.sanitizeUri() : String? {
      val path = decodeURI("UTF-8") ?: decodeURI("ISO-8859-1")
      if (path == null)
          throw Error()

      val localizedPath : String = path.replace('/', File.separatorChar)
      return if (localizedPath.contains(File.separator + ".") ||
                 localizedPath.contains("." + File.separator) ||
                 localizedPath.startsWith(".") || endsWith("."))
           null
      else
        localizedPath
    }

    class StandardPipelineFactory(val config: fun ChannelPipeline.():Unit) : ChannelPipelineFactory {
        override fun getPipeline() : ChannelPipeline {
            val pipeline = DefaultChannelPipeline().with {
                addLast("decoder", HttpRequestDecoder())
                addLast("aggregator", HttpChunkAggregator(65536))
                addLast("encoder", HttpResponseEncoder())
                addLast("chunkedWriter", ChunkedWriteHandler())
            }
            pipeline.config () // can not move it inside with{} because of codegen bug
            return pipeline
        }
    }

    fun ServerBootstrap.configPipeline(config: fun ChannelPipeline.(): Unit) =
        setPipelineFactory(
            StandardPipelineFactory(config)
        )
}

fun main(args : Array<String>) {
    val bootstrap = ServerBootstrap(
        NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()
        )
    )
    bootstrap.configPipeline {
        addLast("handler", HttpStaticFileServerHandler(args[0]))
    }
    bootstrap.bind(InetSocketAddress(8080))
}

class HttpStaticFileServerHandler(val rootDir: String) : SimpleChannelUpstreamHandler() {

    fun messageReceived(ctx : ChannelHandlerContext, e : MessageEvent) {
        val request = e.getMessage() as HttpRequest
        if (request.getMethod() != HttpMethod.GET) {
            ctx.sendError(METHOD_NOT_ALLOWED);
            return;
        }

        val path = request.getUri()?.sanitizeUri()
        if (path == null) {
            ctx.sendError(FORBIDDEN);
            return;
        }

        val file = File(rootDir + File.separator + path)
        if (file.isHidden() || !file.exists()) {
            ctx.sendError(NOT_FOUND)
            return
        }
        if (!file.isFile()) {
            ctx.sendError(FORBIDDEN)
            return
        }

        var raf = RandomAccessFile(file, "r")

        val fileLength = raf.length()

        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, OK)
        response["Content-Length"] = fileLength

        val ch = e.getChannel()
        ch?.write(response)
        ch?.write(DefaultFileRegion(raf.getChannel(), 0, fileLength))
    }
}
