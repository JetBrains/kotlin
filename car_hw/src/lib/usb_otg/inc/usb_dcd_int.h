/**
  ******************************************************************************
  * @file    usb_dcd_int.h
  * @author  MCD Application Team
  * @version V2.0.0
  * @date    22-July-2011
  * @brief   Peripheral Device Interface Layer
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
#ifndef USB_DCD_INT_H__
#define USB_DCD_INT_H__

/* Includes ------------------------------------------------------------------*/
#include "usb_dcd.h"



/** @addtogroup USB_OTG_DRIVER
  * @{
  */
  
/** @defgroup USB_DCD_INT
  * @brief This file is the 
  * @{
  */ 


/** @defgroup USB_DCD_INT_Exported_Defines
  * @{
  */ 

typedef struct _USBD_DCD_INT
{
  uint8_t (* DataOutStage) (USB_OTG_CORE_HANDLE *pdev , uint8_t epnum);
  uint8_t (* DataInStage)  (USB_OTG_CORE_HANDLE *pdev , uint8_t epnum);
  uint8_t (* SetupStage) (USB_OTG_CORE_HANDLE *pdev);
  uint8_t (* SOF) (USB_OTG_CORE_HANDLE *pdev);
  uint8_t (* Reset) (USB_OTG_CORE_HANDLE *pdev);
  uint8_t (* Suspend) (USB_OTG_CORE_HANDLE *pdev);
  uint8_t (* Resume) (USB_OTG_CORE_HANDLE *pdev);
  uint8_t (* IsoINIncomplete) (USB_OTG_CORE_HANDLE *pdev);
  uint8_t (* IsoOUTIncomplete) (USB_OTG_CORE_HANDLE *pdev);  
  
  uint8_t (* DevConnected) (USB_OTG_CORE_HANDLE *pdev);
  uint8_t (* DevDisconnected) (USB_OTG_CORE_HANDLE *pdev);   
  
}USBD_DCD_INT_cb_TypeDef;

extern USBD_DCD_INT_cb_TypeDef *USBD_DCD_INT_fops;
/**
  * @}
  */ 


/** @defgroup USB_DCD_INT_Exported_Types
  * @{
  */ 
/**
  * @}
  */ 

/** @defgroup USB_DCD_INT_Exported_Macros
  * @{
  */ 

#define CLEAR_IN_EP_INTR(epnum,intr) \
  diepint.d32=0; \
  diepint.b.intr = 1; \
  USB_OTG_WRITE_REG32(&pdev->regs.INEP_REGS[epnum]->DIEPINT,diepint.d32);

#define CLEAR_OUT_EP_INTR(epnum,intr) \
  doepint.d32=0; \
  doepint.b.intr = 1; \
  USB_OTG_WRITE_REG32(&pdev->regs.OUTEP_REGS[epnum]->DOEPINT,doepint.d32);

/**
  * @}
  */ 

/** @defgroup USB_DCD_INT_Exported_Variables
  * @{
  */ 
/**
  * @}
  */ 

/** @defgroup USB_DCD_INT_Exported_FunctionsPrototype
  * @{
  */ 

uint32_t USBD_OTG_ISR_Handler (USB_OTG_CORE_HANDLE *pdev);

/**
  * @}
  */ 


#endif // USB_DCD_INT_H__

/**
  * @}
  */ 

/**
  * @}
  */ 
/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/

