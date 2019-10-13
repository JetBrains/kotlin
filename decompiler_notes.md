# Заметки по проекту декомпилятора для Kotlin IR

## ВОПРОСЫ

- FAKE_OVERRIDE если из наследника залезаем в пропертю из родительского Primary конструктора

## TODO-лист

- Дженерики (https://kotlinlang.org/docs/reference/generics.html)
- аргументы по-умолчанию в методах и конструкторах, именованые вызовы с перестановкой аргументов
- кастомные геттер и сеттер
- varargs
- package, import, import as (https://kotlinlang.org/docs/reference/packages.html)
- companion object, именованный object, top-level object

- функции высших порядков
- typealias со стрелкой (для функций высших порядков)
- inner, nested классы
- делегирование (https://kotlinlang.org/docs/reference/delegation.html, https://kotlinlang.org/docs/reference/delegated-properties.html)
- Safe и Unsafe cast operator, Smart casts
- Работа со scope functions - https://kotlinlang.org/docs/reference/scope-functions.html

## LATER

- when с множественным условием через запятую
- return when (varName) - block с WHEN в origin
- использовать abbrevation при выводе типа для typealias
- override fun - проверять overridenSymbols (если visibility и modality не отличаются от родительских, то не пишем их)
- когда у проперти нет BackingField
- как обращаться к исключению в теле catch
- локальные классы

## DONE

- рефакторинг
- секции инициализации
- super и instance вызовы в конструкторе/методе
- primary конструкторы
- наследование (при наследовании отрисовать вызов primary конструктора родителя)
- реализация интерфейсов (в т.ч множественная)
- extension properties
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
