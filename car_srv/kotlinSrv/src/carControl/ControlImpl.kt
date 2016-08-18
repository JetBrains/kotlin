package carControl

import MicroController
import mcTransport
import fs
import setTimeout


/**
 * Created by user on 7/27/16.
 */
class ControlImpl : Control {


    override fun moveCarLeft() {
        println("move left")
        mcTransport.sendBytes(4)
    }

    override fun moveCarRight() {
        println("move rigth")
        mcTransport.sendBytes(3)
    }

    override fun moveCarForward() {
        println("move forward")
        mcTransport.sendBytes(1)
    }

    override fun moveCarBackward() {
        println("move backward")
        mcTransport.sendBytes(2)
    }

    override fun stopCar() {
        println("stopped")
        mcTransport.sendBytes(0)
    }

    override fun delay(ms: Int, callBack: () -> Unit) {
        setTimeout(callBack, ms)
    }
}