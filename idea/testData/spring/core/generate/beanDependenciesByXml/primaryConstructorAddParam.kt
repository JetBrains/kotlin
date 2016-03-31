// ACTION_CLASS: org.jetbrains.kotlin.idea.spring.generate.GenerateKotlinSpringBeanXmlDependencyAction$Constructor
// CONFIG_FILE: primaryConstructorAddParam-config.xml
// CHOOSE_BEAN: bazBean
package a

open class FooBean(barBean: BarBean) {<caret>
}

open class BarBean

open class BazBean