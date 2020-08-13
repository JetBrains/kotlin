# Coroutines Codegen

This document aims to collect every piece of information about coroutines codegen in one place, so, instead of reading the compiler code or
writing snippets and looking at resulting bytecode, a programmer can check the document and find a section which explains how and, more 
importantly, why the compiler behaves like this (or, to be precise, should behave like this). Hopefully, this will help people working on 
the compiler and advanced Kotlin programmers to understand the reasons behind specific design decisions.

The document is JVM-centric, that means it explains how things work in JVM BE since this is the area I am most familiar with and since in 
JVM, there are guaranties of backward compatibility, which the compiler shall obey in both so-called "Old JVM" back-end, as well as in the 
new JVM_IR one. The naming of the new back-end can differ from the official documentation: the document uses the "IR" suffix, while the 
official documentation omits it.

If the name of a section of the document has an "Old JVM:" prefix, it explains old JVM back-end specific details; if the prefix is "JVM_IR," 
then it is JVM_IR back-end specific. If the prefix is plain "JVM," the explanation applies to both the old back-end and the new one. If there 
is no such prefix, the section explains the general behavior of coroutines and shall apply to all back-ends.

The document sticks to release coroutines since we deprecated experimental coroutines in 1.3, and JVM_IR does
not support them. However, there are sections, which explain differences in code generation between release and experimental coroutines 
wherever appropriate, since we technically still support them. Sections, which describe experimental coroutines, have a "1.2" prefix.

If the current implementation is not ideal (or has a bug), there is a description of the difference and the steps to implement the "correct" 
version. These subsections start with "FIXME."

Throughout the document term "coroutine" will represent either a suspend lambda or a suspend function, which is different from the usual 
definition of coroutines - something like a lightweight thread. The document reuses the term since "suspend lambda or function" is wordy, 
and when it requires the typical definition, it says explicitly "a coroutine in a broad sense."

The document often uses the term "undefined behavior," which means that we consciously rejected defining the behavior. Thus, the behavior 
may vary from version to version, from back-end to back-end, and one should use it with extreme caution.

Lastly, most of the examples presented in the document actually suspend, so one is sure every piece is in place since coroutines is a broad
and complex topic, and it is easy to forget one piece, which will lead to a runtime error or even worse, semantically wrong code execution.

