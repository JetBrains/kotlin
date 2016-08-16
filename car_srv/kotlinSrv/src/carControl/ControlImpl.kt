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
        mcTransport.writeToFile(4)
    }

    override fun moveCarRight() {
        println("move rigth")
        mcTransport.writeToFile(3)
    }

    override fun moveCarForward() {
        println("move forward")
        mcTransport.writeToFile(1)
    }

    override fun moveCarBackward() {
        println("move backward")
        mcTransport.writeToFile(2)
    }

    override fun stopCar() {
        println("stopped")
        mcTransport.writeToFile(0)
    }

    override fun delay(ms: Int, callBack: () -> Unit) {
        setTimeout(callBack, ms)
    }
}