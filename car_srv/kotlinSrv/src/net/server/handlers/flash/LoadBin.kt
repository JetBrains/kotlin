package server.handlers.flash

import server.handlers.AbstractHandler
import require

/**
 * Created by user on 7/27/16.
 */
class LoadBin : AbstractHandler {

    val fs: dynamic

    constructor(protoDecoder: dynamic, protoEncoder: dynamic) : super(protoDecoder, protoEncoder) {
        this.fs = require("fs")
    }

    override fun makeResult(message: dynamic, resultList: dynamic, finalCallback: () -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
