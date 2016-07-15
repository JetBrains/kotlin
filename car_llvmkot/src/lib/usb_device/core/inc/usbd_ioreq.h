/**
  ******************************************************************************
  * @file    usbd_ioreq.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    22-July-2011
  * @brief   header file for the usbd_ioreq.c file
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */ 

/* Define to prevent recursive inclusion -------------------------------------*/

#ifndef __USBD_IOREQ_H_
#define __USBD_IOREQ_H_

/* Includes ------------------------------------------------------------------*/
#include  "usbd_def.h"
#include  "usbd_core.h"

/** @addtogroup STM32_USB_OTG_DEVICE_LIBRARY
  * @{
  */
  
/** @defgroup USBD_IOREQ
  * @brief header file for the usbd_ioreq.c file
  * @{
  */ 

/** @defgroup USBD_IOREQ_Exported_Defines
  * @{
  */ 
/**
  * @}
  */ 


/** @defgroup USBD_IOREQ_Exported_Types
  * @{
  */


/**
  * @}
  */ 



/** @defgroup USBD_IOREQ_Exported_Macros
  * @{
  */ 

/**
  * @}
  */ 

/** @defgroup USBD_IOREQ_Exported_Variables
  * @{
  */ 

/**
  * @}
  */ 

/** @defgroup USBD_IOREQ_Exported_FunctionsPrototype
  * @{
  */ 

USBD_Status  USBD_CtlSendData (USB_OTG_CORE_HANDLE  *pdev, 
                               uint8_t *buf,
                               uint16_t len);

USBD_Status  USBD_CtlContinueSendData (USB_OTG_CORE_HANDLE  *pdev, 
                               uint8_t *pbuf,
                               uint16_t len);

USBD_Status USBD_CtlPrepareRx (USB_OTG_CORE_HANDLE  *pdev, 
                               uint8_t *pbuf,                                 
                               uint16_t len);

USBD_Status  USBD_CtlContinueRx (USB_OTG_CORE_HANDLE  *pdev, 
                              uint8_t *pbuf,                                          
                              uint16_t len);

USBD_Status  USBD_CtlSendStatus (USB_OTG_CORE_HANDLE  *pdev);

USBD_Status  USBD_CtlReceiveStatus (USB_OTG_CORE_HANDLE  *pdev);

uint16_t  USBD_GetRxCount (USB_OTG_CORE_HANDLE  *pdev , 
                           uint8_t epnum);

/**
  * @}
  */ 

#endif /* __USBD_IOREQ_H_ */

/**
  * @}
  */ 

/**
* @}
*/ 
/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
