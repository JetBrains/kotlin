#include <stm32f4xx.h>
#include <usbd_cdc_core.h>
#include <usbd_usr.h>
#include <usbd_desc.h>
#include <usb_conf.h>

__ALIGN_BEGIN USB_OTG_CORE_HANDLE USB_OTG_dev __ALIGN_END;

void VCP_init(void)
{
    USBD_Init(&USB_OTG_dev,
            USB_OTG_FS_CORE_ID,
            &USR_desc,
            &USBD_CDC_cb,
            &USR_cb);
}
