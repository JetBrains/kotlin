/**
 * Created by user on 8/18/16.
 */
interface MCConnectObserver<V> {

    fun connect(vararg params: V)
    fun disconnect()

}