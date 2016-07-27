package carControl

import require
import transportFilePath


/**
 * Created by user on 7/27/16.
 */
class ControlImpl : Control {

    val fs: dynamic;

    init {
        this.fs = require("fs")
    }

    override fun moveCarLeft() {
        println("move left")
        writeToFile(4)
    }

    override fun moveCarRight() {
        println("move rigth")
        writeToFile(3)
    }

    override fun moveCarForward() {
        println("move forward")
        writeToFile(1)
    }

    override fun moveCarBackward() {
        println("move backward")
        writeToFile(2)
    }

    override fun stopCar() {
        println("stopped")
        writeToFile(0)
    }

    override fun delay(ms: Int, callBack: () -> Unit) {
        setTimeOut(callBack, ms)
    }

    @native
    fun setTimeOut(callBack: () -> Unit, ms: Int) {

    }


    fun writeToFile(byte: Byte) {
        fs.appendFile(transportFilePath, byte.toString(), "binary", { err ->
            if (err) {
                println("error")
            } else {
                println("ok")
            }
        })
    }

}