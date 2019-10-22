# Заметки по проекту декомпилятора для Kotlin IR

## ВОПРОСЫ

- FAKE_OVERRIDE если из наследника залезаем в пропертю из родительского Primary конструктора
- Как получить имя класса/инстанса, ссылку на метод которого используем через ::
- Когда нам нужно протаскивать информацию о data в глубину визитора (например, return для лямбды)

## TODO-лист

- функции высших порядков и лямбды (https://kotlinlang.org/docs/reference/lambdas.html): **надо больше тестов**
- Работа со scope functions - https://kotlinlang.org/docs/reference/scope-functions.html **надо больше тестов**
- 
- cast операторы as, as?
- делегирование (https://kotlinlang.org/docs/reference/delegation.html, https://kotlinlang.org/docs/reference/delegated-properties.html)
- typealias со стрелкой (для функций высших порядков)
- кастомные геттер и сеттер
- продолжить дженерики (https://kotlinlang.org/docs/reference/generics.html)
- обход дерева и сбор импортов через full qualified names

## LATER

1 очередь:
- companion object, именованный object, top-level object
- inner, nested классы.
- when с множественным условием через запятую
- return when (varName) - block с WHEN в origin

- расстановка скобок для логических операторов !, &&, ||
- использовать abbrevation при выводе типа для typealias
- override fun - проверять overridenSymbols (если visibility и modality не отличаются от родительских, то не пишем их)
- когда у проперти нет BackingField
- как обращаться к исключению в теле catch
- локальные классы

## DONE

- nullable типы
- enum
- break, continue
- package
- аргументы по-умолчанию в методах, именованые вызовы с перестановкой аргументов
- data class + destructing declarations
- varargs
- аргументы по-умолчанию в конструкторах, именованые вызовы с перестановкой аргументов
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
