package android.app

public open class Activity {
    public open fun findViewById(@Suppress("UNUSED_PARAMETER") id: Int): android.view.View = null!!
}

public open class Fragment {
    public open fun getView(): android.view.View = null!!
}

open class Dialog {
    open fun findViewById(id: Int): android.view.View? = null
}
