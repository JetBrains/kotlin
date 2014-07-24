package android.app
import android.view.View

abstract class Activity {
    open abstract fun findViewById(id: Int): View?
}