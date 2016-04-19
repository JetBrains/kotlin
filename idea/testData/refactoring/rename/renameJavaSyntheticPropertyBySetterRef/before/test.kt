fun test(bean: Bean) {
    bean./*rename*/prop = "a"
    println(bean.prop)
    bean.prop += "a"
}