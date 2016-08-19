/**
 * Created by user on 8/18/16.
 */
interface MCConnectObservable<V> {

    fun addObserver(MCConnectObserver: MCConnectObserver<V>)
    fun removeObserver(MCConnectObserver: MCConnectObserver<V>)

}