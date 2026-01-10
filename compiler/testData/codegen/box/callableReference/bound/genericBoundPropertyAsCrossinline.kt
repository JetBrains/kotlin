// KT-30629

// FILE: lib.kt
abstract class BaseFragment<T : BaseViewModel> {
    lateinit var viewModel: T

    open fun onActivityCreated(): String {
        viewModel = retrieveViewModel()
        return "Fail"
    }

    abstract fun retrieveViewModel(): T
}

class DerivedFragment : BaseFragment<DerivedViewModel>() {
    override fun onActivityCreated(): String {
        super.onActivityCreated()

        return bind(viewModel::property)
    }

    override fun retrieveViewModel(): DerivedViewModel = DerivedViewModel()
    inline fun <T> bind(crossinline viewModelGet: () -> T?): String {
        return setOnFocusChangeListener { viewModelGet() as String }
    }

    fun setOnFocusChangeListener(l: () -> String): String {
        return l()
    }
}

abstract class BaseViewModel
class DerivedViewModel : BaseViewModel() {
    var property: String? = "OK"
}

// FILE: main.kt
fun box(): String {
    return DerivedFragment().onActivityCreated()
}
