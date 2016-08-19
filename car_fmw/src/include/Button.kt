external fun car_user_btn_init(handler: () -> Unit)
external fun car_user_btn_is_pushed(): Boolean
external fun EXTI0_IRQHandler()

object Button {
    fun setCallback(handler: () -> Unit) {
        car_user_btn_init(handler)
    }

    fun isPushed(): Boolean = car_user_btn_is_pushed()
}