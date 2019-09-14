# Заметки по проекту декомпилятора для Kotlin IR

## Календарь

12.09.2019 - прикрутить blackbox тесты
13.09.2019 - сделать конструкторы --- primaryConstructor.kt, secondaryConstructors.kt, secondaryConstructorWithInitializersFromClassBody.kt
             сделать мемберы --- abstractMembers.kt, classMembers.kt, 
14.09.2019 - сделать init блоки (инициализация var и val, вызовы) --- initBlock.kt, initVal.kt, initVar.kt 
             super вызовы ---  superCalls.kt, qualifiedSuperCalls.kt
             object, companion object --- companionObject.kt, objectWithInitializers.kt, 

15.09.2019 - сделать sealed class --- sealedClasses.kt
             data class --- dataClasses.kt, dataClassesGeneric.kt, dataClassWithArrayMembers.kt, 
             enum --- enum.kt, enumClassModality.kt, enumWithSecondaryCtor.kt, 
             inner классы --- innerClass.kt, innerClassWithDelegatingConstructor.kt, outerClassAccess.kt
             local классы --- localClasses.kt, 

## TODO-лист
