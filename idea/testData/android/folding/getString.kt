package com.myapp

import <fold text='...' expand='false'>android.content.Context
import com.myapp.R.string.*;</fold>

fun Context.getAppTitle() = <fold text='"My Cool Application"' expand='false'>getString(R.string.app_title)</fold>

fun getAppTitle(context: Context) <fold text='{...}' expand='true'>{
    return <fold text='"My Cool Application"' expand='false'>context.getString(app_title)</fold>
}</fold>

fun getLongString(context: Context) = <fold text='"Some very very long string which should be trimmed. Lor..."' expand='false'>context.getString(R.string.long_string)</fold>

fun bar(text: String) = text

fun foo(context: Context) <fold text='{...}' expand='true'>{
    val name = "Vasya"
    val otherName = context.getString(R.string.no_resource)
    val cancel = context.getString(android.R.string.cancel)
    val text = <fold text='"Hello {Vasya}!"' expand='false'>context.getString(R.string.format_string, name)</fold>
    val text = <fold text='"Hello {otherName}!"' expand='false'>context.getString(R.string.format_string, otherName)</fold>
    val emptyString = <fold text='""' expand='false'>context.getString(R.string.empty_string)</fold>
    val someInt = <fold text='some_int: 8' expand='false'>context.resources.getInteger(R.integer.some_int)</fold>
    context.getString(R.string.no_resource)
    bar(<fold text='"Third: {333} Repeated: {333} First: {111} Second: {222}"' expand='false'>context.getResources().getString(R.string.compex_format_string, "111", "222", "333")</fold>)
    val invalid = with(context) { getString(<fold text='"Escaped: %s First: %1$s Invalid: %20$s"' expand='false'>R.string.invalid_format</fold>, name, otherName) }
}</fold>
