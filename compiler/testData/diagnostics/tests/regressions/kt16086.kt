// FILE: com/winterbe/domain/IEntity.java
package com.winterbe.domain;
import com.winterbe.observer.ObserverSupport;

public interface IEntity {
    ObserverSupport getObserverSupport();
}

// FILE: 1.kt
package com.winterbe.observer
import com.winterbe.domain.IEntity

abstract class Observer : List<IEntity>


// FILE: 2.kt
package com.winterbe.observer
import com.winterbe.domain.IEntity

class ObserverSupport<T : IEntity>(private val observers: List<Observer>)
