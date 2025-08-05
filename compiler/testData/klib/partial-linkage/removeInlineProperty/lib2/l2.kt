fun getterDirectCall1() = topLevelProperty

fun getterDirectCall2() = with("") { "".topLevelPropertyWithReceiver }

fun getterDirectCall3() = C().classProperty

fun getterDirectCall4() = C().run { with("") { "".classPropertyWithReceiver } }

inline fun getterInlineCall1() = topLevelProperty

inline fun getterInlineCall2() = with("") { "".topLevelPropertyWithReceiver }

inline fun getterInlineCall3() = C().classProperty

inline fun getterInlineCall4() = C().run { with("") { "".classPropertyWithReceiver } }

inline fun callGetterFromLambda(f: () -> String) = f()

inline fun getterLambdaCall1() = callGetterFromLambda { topLevelProperty }

inline fun getterLambdaCall2() = callGetterFromLambda { with("") { "".topLevelPropertyWithReceiver } }

inline fun getterLambdaCall3() = callGetterFromLambda { C().classProperty }

inline fun getterLambdaCall4() = callGetterFromLambda { C().run { with("") { "".classPropertyWithReceiver } } }

fun setterDirectCall1() { topLevelProperty = "value" }

fun setterDirectCall2() { with("") { "".topLevelPropertyWithReceiver = "value" } }

fun setterDirectCall3() { C().classProperty = "value" }

fun setterDirectCall4() { C().run { with("") { "".classPropertyWithReceiver = "value" } } }

inline fun setterInlineCall1() { topLevelProperty = "value" }

inline fun setterInlineCall2() { with("") { "".topLevelPropertyWithReceiver = "value" } }

inline fun setterInlineCall3() { C().classProperty = "value" }

inline fun setterInlineCall4() { C().run { with("") { "".classPropertyWithReceiver = "value" } } }

inline fun callSetterFromLambda(f: () -> Unit) = f()

inline fun setterLambdaCall1() = callSetterFromLambda { topLevelProperty = "value" }

inline fun setterLambdaCall2() = callSetterFromLambda { with("") { "".topLevelPropertyWithReceiver = "value" } }

inline fun setterLambdaCall3() = callSetterFromLambda { C().classProperty = "value" }

inline fun setterLambdaCall4() = callSetterFromLambda { C().run { with("") { "".classPropertyWithReceiver = "value" } } }