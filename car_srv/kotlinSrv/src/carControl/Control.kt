package carControl

/**
 * Created by user on 7/27/16.
 */
interface Control {

    fun moveCarLeft()
    fun moveCarRight()
    fun moveCarForward()
    fun moveCarBackward()

    fun stopCar()

    fun delay(ms: Int, callBack:()->Unit)

}