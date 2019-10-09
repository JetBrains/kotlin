# Заметки по проекту декомпилятора для Kotlin IR

## ВОПРОСЫ

- почему при использовании try/catch как выражения после return у finallyExpression тип - IrTypeOperatorCall?
- типы выводятся оригинальные, а не typealias. Получается, что в декомпиляторе от typealias толку нет - abbrevation
- when с множественным условием через запятую - м.б. изменить на последовательные EQEQ бранчи?
- override fun - становится open? как определить, что бы override, достаточно ли для этого overridenSymbols проверить (если visibility и modality не отличаются от родительских, то не пишем)

## TODO-лист

- наследование
- super и instance вызовы в конструкторе/методе
- секции инициализации (нужно ли отображать IrInstanceInitializerCall?)
- кастомные геттер и сеттер
- аргументы по-умолчанию в методах и конструкторах
- extension properties
- вложенные классы
- локальные классы
- companion object, именованный object, top-level object
- package, import, import as (https://kotlinlang.org/docs/reference/packages.html)
- делегирование (https://kotlinlang.org/docs/reference/delegation.html, https://kotlinlang.org/docs/reference/delegated-properties.html)
- Safe и Unsafe cast operator, Smart casts
- Работа со scope functions - https://kotlinlang.org/docs/reference/scope-functions.html
- Дженерики (https://kotlinlang.org/docs/reference/generics.html)

## LATER

- typealias со стрелкой (для функций высших порядков)
- when с множественным условием через запятую
- return when (varName) - block с WHEN в origin
- 
- когда у проперти нет BackingField
- как обращаться к исключению в теле catch
- почему box падает при OK в finally

## DONE

- оператор is, !is в ветке when
- создание экземпляров класса через вызовы конструкторов
- получение свойства экземпляра, инициализированной в конструкторе
- try/catch
- typealias
- интерполяция строк для getValue и костант (надо придумать как унифицировать, а не через when)


В интерфейсах можно не обрабатывать modality
return try - ветка finally без явного return не возвращается 
IrDelegatingConstructorCall 
  - у primary - только отличный от Any родитель (запись : ParentClass())
  - secondary конструктор по цепочке должен доделегироваться до primary
  - secondary - если нет primary, то 
для return when(...) использовать return run{...}
